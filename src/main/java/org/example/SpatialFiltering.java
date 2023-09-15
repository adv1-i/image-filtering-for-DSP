package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.List;
import java.awt.Point;

public class SpatialFiltering {
    private enum NoiseType { UNIPOLAR, BIPOLAR }
    private enum Channel { R, G, B }
    private enum FilterType { MEDIAN, HARMONIC_MEAN }

    private BufferedImage originalImage;
    private BufferedImage noisyImage;
    private BufferedImage channelImage;
    private BufferedImage filteredImage;

    private JLabel originalImageLabel;
    private JLabel noisyImageLabel;
    private JLabel channelImageLabel;
    private JLabel filteredImageLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new SpatialFiltering().createAndShowGUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void createAndShowGUI() throws IOException {
        JFrame frame = new JFrame("Image Noise");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.PAGE_START);

        JButton openButton = new JButton("Открыть изображение");
        openButton.addActionListener(e -> openImage(frame));
        panel.add(openButton);

        JComboBox<NoiseType> noiseTypeComboBox = new JComboBox<>(NoiseType.values());
        panel.add(noiseTypeComboBox);

        JComboBox<Channel> channelComboBox = new JComboBox<>(Channel.values());
        panel.add(channelComboBox);


        JButton applyButton = new JButton("Применить шум и выделить канал");
        applyButton.addActionListener(e -> {
            if (originalImage != null) {
                noisyImage = copyImage(originalImage);
                addNoise(noisyImage, (NoiseType) noiseTypeComboBox.getSelectedItem());
                noisyImageLabel.setIcon(new ImageIcon(noisyImage));

                channelImage = copyImage(noisyImage);
                extractChannel(channelImage, (Channel) channelComboBox.getSelectedItem());
                channelImageLabel.setIcon(new ImageIcon(channelImage));
            }
        });
        panel.add(applyButton);

        JComboBox<FilterType> filterTypeComboBox = new JComboBox<>(FilterType.values());
        panel.add(filterTypeComboBox);

        SpinnerNumberModel model = new SpinnerNumberModel(3, //initial value
                3, //min
                11, //max
                2); //step
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0'x'"));
        panel.add(spinner);


        JButton applyNoiseFilterButton = new JButton("Применить фильтр");
        applyNoiseFilterButton.addActionListener(e -> {
            if (noisyImage != null) {
                filteredImage = copyImage(noisyImage);
                int maskSize = (Integer) spinner.getValue();
                if (filterTypeComboBox.getSelectedItem() == FilterType.MEDIAN) {
                    applyMedianFilter(filteredImage, maskSize);
                } else {  // FilterType.HARMONIC_MEAN
                    applyHarmonicMeanFilter(filteredImage, maskSize);
                }
                filteredImageLabel.setIcon(new ImageIcon(filteredImage));
            }
        });
        panel.add(applyNoiseFilterButton);

        panel.add(applyNoiseFilterButton);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        JButton saveAllButton = new JButton("Сохранить изображения");
        saveAllButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();

            if (noisyImage != null) {
                JOptionPane.showMessageDialog(frame, "Сохранение зашумленного изображения");
                fileChooser.setSelectedFile(new File("noise-image-01.png"));
                if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        ImageIO.write(noisyImage, "png", file);
                        JOptionPane.showMessageDialog(frame, "Зашумленное изображение сохранено как " + file.getName());
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }

            if (channelImage != null) {
                JOptionPane.showMessageDialog(frame, "Сохранение зашумленного изображения по каналу");
                fileChooser.setSelectedFile(new File("channel-image-01.png"));
                if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        ImageIO.write(channelImage, "png", file);
                        JOptionPane.showMessageDialog(frame, "Зашумленное изображение по каналу сохранено как " + file.getName());
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }

            if (filteredImage != null) {
                JOptionPane.showMessageDialog(frame, "Сохранение отфильтрованного изображения");
                fileChooser.setSelectedFile(new File("filter-image-01.png"));
                if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        ImageIO.write(filteredImage, "png", file);
                        JOptionPane.showMessageDialog(frame, "Отфильтрованное изображение сохранено как " + file.getName());
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        });
        buttonPanel.add(saveAllButton);



        JButton clearButton = new JButton("Очистить изображения");
        clearButton.addActionListener(e -> {
            originalImageLabel.setIcon(null);
            noisyImageLabel.setIcon(null);
            channelImageLabel.setIcon(null);
            filteredImageLabel.setIcon(null);
            originalImage = null;
            noisyImage = null;
            channelImage = null;
            filteredImage = null;
        });
        buttonPanel.add(clearButton);

        JPanel imagePanel = new JPanel(new GridLayout(1, 4));
        frame.getContentPane().add(imagePanel, BorderLayout.CENTER);

        originalImageLabel = new JLabel();
        imagePanel.add(new JScrollPane(originalImageLabel));

        noisyImageLabel = new JLabel();
        imagePanel.add(new JScrollPane(noisyImageLabel));

        channelImageLabel = new JLabel();
        imagePanel.add(new JScrollPane(channelImageLabel));

        filteredImageLabel = new JLabel();
        imagePanel.add(new JScrollPane(filteredImageLabel));

        frame.pack();
        frame.setVisible(true);
    }

    private void openImage(JFrame frame) {
        JFileChooser fileChooser = new JFileChooser();

        // Создаем фильтр для файлов с расширениями png, jpg, jpeg и bmp
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image files", "png", "jpg", "jpeg", "bmp");
        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                originalImage = ImageIO.read(selectedFile);
                originalImageLabel.setIcon(new ImageIcon(originalImage));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addNoise(BufferedImage image, NoiseType noiseType) {
        Random rand = new Random();

        // Вычисляем количество пикселей, которые нужно зашумить (10% от общего количества пикселей)
        int amount = (int) (image.getWidth() * image.getHeight() * 0.1);
        // Каждый объект Point содержит две координаты: x и y, которые соответствуют позиции пикселя на изображении.
        List<Point> points = new ArrayList<>();

        // Создаем список всех пикселей на изображении
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                points.add(new Point(x, y));
            }
        }

        // Перемешиваем список, чтобы выбрать случайные пиксели для зашумления
        Collections.shuffle(points, rand);

        // Берем первые 10% пикселей из перемешанного списка
        List<Point> noisePoints = points.subList(0, amount);

        // Добавляем шум к выбранным пикселям
        for (Point p : noisePoints) {
            Color color;
            if (noiseType == NoiseType.UNIPOLAR) {
                color = Color.WHITE;  // Светлая точка
            } else {  // Bipolar noise
                color = rand.nextBoolean() ? Color.BLACK : Color.WHITE;
            }

            // Устанавливаем цвет пикселя на изображении
            image.setRGB(p.x, p.y, color.getRGB());
        }
    }



    // Метод для извлечения определенного цветового канала из изображения
    private void extractChannel(BufferedImage image, Channel channel) {
        // Проходим по каждому пикселю изображения
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                // Получаем цвет текущего пикселя
                Color color = new Color(image.getRGB(x, y));

                // В зависимости от выбранного канала зануляем два других канала
                if (channel == Channel.R) {
                    color = new Color(color.getRed(), 0, 0);  // Оставляем только красный канал
                } else if (channel == Channel.G) {
                    color = new Color(0, color.getGreen(), 0);  // Оставляем только зеленый канал
                } else {  // Channel.B
                    color = new Color(0, 0, color.getBlue());  // Оставляем только синий канал
                }

                // Устанавливаем новый цвет для текущего пикселя в изображении
                image.setRGB(x, y, color.getRGB());
            }
        }
    }


    private void applyMedianFilter(BufferedImage image, int maskSize) {
        // Создаем копию исходного изображения
        BufferedImage copy = copyImage(image);

        // Вычисляем смещение, которое равно половине размера маски
        int offset = maskSize / 2;

        // Проходим по каждому пикселю изображения, начиная от смещения и заканчивая высотой/шириной изображения минус смещение
        for (int y = offset; y < image.getHeight() - offset; y++) {
            for (int x = offset; x < image.getWidth() - offset; x++) {
                // Инициализируем массивы для каждого цветового канала
                int[] reds = new int[maskSize * maskSize];
                int[] greens = new int[maskSize * maskSize];
                int[] blues = new int[maskSize * maskSize];

                // Проходим по каждому пикселю в окрестности текущего пикселя
                for (int ky = -offset; ky <= offset; ky++) {
                    for (int kx = -offset; kx <= offset; kx++) {
                        // Получаем цвет пикселя из копии изображения
                        Color color = new Color(copy.getRGB(x + kx, y + ky));
                        // Добавляем значение цвета в соответствующий массив
                        reds[(ky + offset) * maskSize + kx + offset] = color.getRed();
                        greens[(ky + offset) * maskSize + kx + offset] = color.getGreen();
                        blues[(ky + offset) * maskSize + kx + offset] = color.getBlue();
                    }
                }

                // Сортируем массивы
                Arrays.sort(reds);
                Arrays.sort(greens);
                Arrays.sort(blues);

                // Вычисляем медианное значение для каждого цветового канала
                Color medianColor = new Color(reds[reds.length / 2], greens[greens.length / 2], blues[blues.length / 2]);

                // Устанавливаем новый цвет для текущего пикселя в исходном изображении
                image.setRGB(x, y, medianColor.getRGB());
            }
        }
    }


    private void applyHarmonicMeanFilter(BufferedImage image, int maskSize) {
        // Создаем копию исходного изображения
        BufferedImage copy = copyImage(image);

        // Вычисляем смещение, которое равно половине размера маски
        int offset = maskSize / 2;

        // Проходим по каждому пикселю изображения, начиная от смещения и заканчивая высотой/шириной изображения минус смещение
        for (int y = offset; y < image.getHeight() - offset; y++) {
            for (int x = offset; x < image.getWidth() - offset; x++) {
                // Инициализируем суммы для каждого цветового канала
                double sumRed = 0, sumGreen = 0, sumBlue = 0;

                // Проходим по каждому пикселю в окрестности текущего пикселя
                for (int ky = -offset; ky <= offset; ky++) {
                    for (int kx = -offset; kx <= offset; kx++) {
                        // Получаем цвет пикселя из копии изображения
                        Color color = new Color(copy.getRGB(x + kx, y + ky));
                        // Добавляем обратное значение цвета к сумме (прибавляем 1, чтобы избежать деления на ноль)
                        sumRed += 1.0 / (color.getRed() + 1);
                        sumGreen += 1.0 / (color.getGreen() + 1);
                        sumBlue += 1.0 / (color.getBlue() + 1);
                    }
                }

                // Вычисляем среднегармоническое значение для каждого цветового канала
                int meanRed = (int) ((maskSize * maskSize) / sumRed);
                int meanGreen = (int) ((maskSize * maskSize) / sumGreen);
                int meanBlue = (int) ((maskSize * maskSize) / sumBlue);

                // Ограничиваем значения в диапазоне [0, 255]
                meanRed = Math.min(Math.max(meanRed, 0), 255);
                meanGreen = Math.min(Math.max(meanGreen, 0), 255);
                meanBlue = Math.min(Math.max(meanBlue, 0), 255);

                // Создаем новый цвет на основе вычисленных значений
                Color meanColor = new Color(meanRed, meanGreen, meanBlue);

                // Устанавливаем новый цвет для текущего пикселя в исходном изображении
                image.setRGB(x, y, meanColor.getRGB());
            }
        }
    }

    // Метод для создания копии изображения
    private BufferedImage copyImage(BufferedImage source) {
        // Получаем модель цвета исходного изображения
        ColorModel colorModel = source.getColorModel();

        // Проверяем, является ли альфа-канал исходного изображения предварительно умноженным
        boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();

        // Создаем копию растра (данных пикселей) исходного изображения
        WritableRaster raster = source.copyData(null);

        // Создаем новое изображение с той же моделью цвета, копией растра и тем же состоянием альфа-канала
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }

}



